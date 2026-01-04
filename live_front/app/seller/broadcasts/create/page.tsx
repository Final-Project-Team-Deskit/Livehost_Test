"use client"

import type React from "react"
import Image from "next/image"
import { useState } from "react"
import { useRouter } from "next/navigation"
import { ArrowLeft, Plus, Trash2, Upload, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"
import { ProductPickerModal } from "@/components/seller/product-picker-modal"
import { TermsModal } from "@/components/seller/terms-modal"
import type { Product } from "@/lib/admin-mock-data"

interface BroadcastProduct extends Product {
  broadcastPrice: number
  quantity: number
}

export default function CreateBroadcastPage() {
  const router = useRouter()
  const [step, setStep] = useState(1)
  const [qCards, setQCards] = useState<string[]>([""])
  const [title, setTitle] = useState("")
  const [category, setCategory] = useState("")
  const [notice, setNotice] = useState("")

  const [startDate, setStartDate] = useState("")
  const [startTime, setStartTime] = useState("")

  const [selectedProducts, setSelectedProducts] = useState<BroadcastProduct[]>([])
  const [agreedToTerms, setAgreedToTerms] = useState(false)

  const [productPickerOpen, setProductPickerOpen] = useState(false)
  const [termsModalOpen, setTermsModalOpen] = useState(false)

  const [thumbnailPreview, setThumbnailPreview] = useState<string | null>(null)
  const [waitingScreenPreview, setWaitingScreenPreview] = useState<string | null>(null)

  const getReservableSlots = () => {
    const slots: string[] = []
    const now = new Date()

    for (let hour = 9; hour <= 22; hour++) {
      const slotTime = `${hour.toString().padStart(2, "0")}:00`
      // Mock: check if slot has < 3 reservations
      const reservationCount = Math.floor(Math.random() * 4)
      if (reservationCount < 3) {
        slots.push(slotTime)
      }
    }
    return slots
  }

  const reservableSlots = getReservableSlots()

  const handleAddQCard = () => {
    setQCards([...qCards, ""])
  }

  const handleRemoveQCard = (index: number) => {
    setQCards(qCards.filter((_, i) => i !== index))
  }

  const handleQCardChange = (index: number, value: string) => {
    const newQCards = [...qCards]
    newQCards[index] = value
    setQCards(newQCards)
  }

  const handleSubmit = () => {
    if (!title || !category || !startDate || !startTime || selectedProducts.length === 0) {
      alert("필수 항목을 모두 입력해주세요.")
      return
    }

    if (!agreedToTerms) {
      alert("약관을 체크하지 않으면 방송을 등록할 수 없습니다.")
      return
    }

    alert("방송이 등록되었습니다!")
    router.push("/seller/broadcasts")
  }

  const updateProductPrice = (productId: string, newPrice: number) => {
    setSelectedProducts((prev) => prev.map((p) => (p.id === productId ? { ...p, broadcastPrice: newPrice } : p)))
  }

  const updateProductQuantity = (productId: string, newQuantity: number) => {
    setSelectedProducts((prev) => prev.map((p) => (p.id === productId ? { ...p, quantity: newQuantity } : p)))
  }

  const removeProduct = (productId: string) => {
    setSelectedProducts((prev) => prev.filter((p) => p.id !== productId))
  }

  const handleThumbnailUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (!file.type.match(/image\/(jpeg|png)/)) {
      alert("JPG 또는 PNG 형식만 업로드 가능합니다.")
      return
    }

    if (file.size > 5 * 1024 * 1024) {
      alert("썸네일은 최대 5MB까지 업로드 가능합니다.")
      return
    }

    const reader = new FileReader()
    reader.onloadend = () => {
      setThumbnailPreview(reader.result as string)
    }
    reader.readAsDataURL(file)
  }

  const handleWaitingScreenUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (!file.type.match(/image\/(jpeg|png)/)) {
      alert("JPG 또는 PNG 형식만 업로드 가능합니다.")
      return
    }

    if (file.size > 7 * 1024 * 1024) {
      alert("대기화면은 최대 7MB까지 업로드 가능합니다.")
      return
    }

    const reader = new FileReader()
    reader.onloadend = () => {
      setWaitingScreenPreview(reader.result as string)
    }
    reader.readAsDataURL(file)
  }

  if (step === 1) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-3xl">
        <div className="space-y-6">
          <div className="flex items-center gap-4">
            <Button variant="ghost" size="icon" onClick={() => router.back()}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <h1 className="text-2xl font-bold">방송 등록 - 큐 카드 작성</h1>
          </div>

          <Card className="p-6 space-y-4">
            <div className="space-y-4">
              {qCards.map((card, index) => (
                <div key={index} className="flex gap-2">
                  <div className="flex-1">
                    <Label>질문 {index + 1}</Label>
                    <Input
                      value={card}
                      onChange={(e) => handleQCardChange(index, e.target.value)}
                      placeholder="방송 중 참고할 질문을 입력하세요"
                    />
                  </div>
                  {qCards.length > 1 && (
                    <Button variant="ghost" size="icon" onClick={() => handleRemoveQCard(index)} className="mt-6">
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  )}
                </div>
              ))}
            </div>

            <Button variant="outline" onClick={handleAddQCard} className="w-full bg-transparent">
              <Plus className="mr-2 h-4 w-4" />
              질문 추가
            </Button>
          </Card>

          <div className="flex items-center justify-between border-t pt-4">
            <div className="text-sm text-muted-foreground">1 / 2</div>
            <Button onClick={() => setStep(2)}>다음 단계</Button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl">
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => setStep(1)}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-bold">방송 등록 - 기본 정보</h1>
        </div>

        <Card className="p-6 space-y-6">
          <div className="space-y-2">
            <Label>
              방송 제목 <span className="text-red-500">*</span>
            </Label>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="방송 제목을 입력하세요 (최대 30자)"
              maxLength={30}
            />
            <p className="text-xs text-muted-foreground">{title.length}/30</p>
          </div>

          <div className="space-y-2">
            <Label>
              카테고리 <span className="text-red-500">*</span>
            </Label>
            <Select value={category} onValueChange={setCategory}>
              <SelectTrigger>
                <SelectValue placeholder="카테고리를 선택하세요" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="가구">가구</SelectItem>
                <SelectItem value="전자기기">전자기기</SelectItem>
                <SelectItem value="패션">패션</SelectItem>
                <SelectItem value="뷰티">뷰티</SelectItem>
                <SelectItem value="악세사리">악세사리</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>공지사항</Label>
            <Textarea
              value={notice}
              onChange={(e) => setNotice(e.target.value)}
              placeholder="시청자에게 공지할 내용을 입력하세요 (최대 50자)"
              maxLength={50}
              rows={3}
            />
            <p className="text-xs text-muted-foreground">{notice.length}/50</p>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>
                방송 날짜 <span className="text-red-500">*</span>
              </Label>
              <Input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                min={new Date().toISOString().split("T")[0]}
              />
            </div>

            <div className="space-y-2">
              <Label>
                방송 시간 <span className="text-red-500">*</span>
              </Label>
              <Select value={startTime} onValueChange={setStartTime}>
                <SelectTrigger>
                  <SelectValue placeholder="시간 선택" />
                </SelectTrigger>
                <SelectContent>
                  {Array.from({ length: 24 }, (_, i) => (
                    <SelectItem key={i} value={`${i.toString().padStart(2, "0")}:00`}>
                      {i.toString().padStart(2, "0")}:00
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>
                판매 상품 등록 <span className="text-red-500">*</span>
              </Label>
              <Button variant="outline" size="sm" onClick={() => setProductPickerOpen(true)}>
                <Plus className="mr-2 h-4 w-4" />
                판매상품 불러오기
              </Button>
            </div>
            {selectedProducts.length > 0 && (
              <div className="border rounded-lg overflow-hidden mt-4">
                <table className="w-full text-sm">
                  <thead className="bg-muted">
                    <tr>
                      <th className="p-2 text-left w-12"></th>
                      <th className="p-2 text-left">상품명</th>
                      <th className="p-2 text-right">실재고</th>
                      <th className="p-2 text-right">판매가</th>
                      <th className="p-2 text-right">방송 할인 가격</th>
                      <th className="p-2 text-right">방송 판매수량</th>
                      <th className="p-2 w-12"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedProducts.map((product) => (
                      <tr key={product.id} className="border-t">
                        <td className="p-2">
                          <div className="w-10 h-10 bg-muted rounded flex-shrink-0 relative overflow-hidden">
                            {product.imageUrl && (
                              <Image
                                src={product.imageUrl || "/placeholder.svg"}
                                alt={product.name}
                                fill
                                className="object-cover"
                              />
                            )}
                          </div>
                        </td>
                        <td className="p-2 font-medium">{product.name}</td>
                        <td className="p-2 text-right">{product.stock}</td>
                        <td className="p-2 text-right">{product.price.toLocaleString()}원</td>
                        <td className="p-2">
                          <Input
                            type="number"
                            value={product.broadcastPrice}
                            onChange={(e) => updateProductPrice(product.id, Number(e.target.value))}
                            className="w-28 text-right"
                            min="0"
                          />
                        </td>
                        <td className="p-2">
                          <Input
                            type="number"
                            value={product.quantity}
                            onChange={(e) => updateProductQuantity(product.id, Number(e.target.value))}
                            className="w-20 text-right"
                            min="1"
                            max={product.stock}
                          />
                        </td>
                        <td className="p-2">
                          <Button variant="ghost" size="icon" onClick={() => removeProduct(product.id)}>
                            <X className="h-4 w-4" />
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <p className="text-xs text-muted-foreground">
              {selectedProducts.length}/10 개 선택됨 (최소 1개, 최대 10개)
            </p>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>
                썸네일 <span className="text-red-500">*</span>
              </Label>
              <label className="block cursor-pointer">
                <input type="file" accept="image/jpeg,image/png" className="hidden" onChange={handleThumbnailUpload} />
                <div className="border-2 border-dashed rounded-lg p-4 text-center hover:border-primary transition-colors aspect-[9/16] flex items-center justify-center">
                  {thumbnailPreview ? (
                    <div className="relative w-full h-full">
                      <Image
                        src={thumbnailPreview || "/placeholder.svg"}
                        alt="썸네일 미리보기"
                        fill
                        className="object-cover rounded"
                      />
                    </div>
                  ) : (
                    <div className="flex flex-col items-center gap-2">
                      <Upload className="h-8 w-8 text-muted-foreground" />
                      <p className="text-sm text-muted-foreground">클릭하여 업로드</p>
                      <p className="text-xs text-muted-foreground">9:16 비율, JPG/PNG, 최대 5MB</p>
                    </div>
                  )}
                </div>
              </label>
            </div>

            <div className="space-y-2">
              <Label>대기화면</Label>
              <label className="block cursor-pointer">
                <input
                  type="file"
                  accept="image/jpeg,image/png"
                  className="hidden"
                  onChange={handleWaitingScreenUpload}
                />
                <div className="border-2 border-dashed rounded-lg p-4 text-center hover:border-primary transition-colors aspect-[9/16] flex items-center justify-center">
                  {waitingScreenPreview ? (
                    <div className="relative w-full h-full">
                      <Image
                        src={waitingScreenPreview || "/placeholder.svg"}
                        alt="대기화면 미리보기"
                        fill
                        className="object-cover rounded"
                      />
                    </div>
                  ) : (
                    <div className="flex flex-col items-center gap-2">
                      <Upload className="h-8 w-8 text-muted-foreground" />
                      <p className="text-sm text-muted-foreground">클릭하여 업로드</p>
                      <p className="text-xs text-muted-foreground">9:16 비율, JPG/PNG, 최대 7MB</p>
                    </div>
                  )}
                </div>
              </label>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <Checkbox id="terms" checked={agreedToTerms} onCheckedChange={(checked) => setAgreedToTerms(!!checked)} />
            <label htmlFor="terms" className="text-sm flex items-center gap-2">
              방송 운영 정책에 동의합니다 <span className="text-red-500">*</span>
              <Button
                variant="link"
                size="sm"
                className="h-auto p-0 text-primary"
                onClick={(e) => {
                  e.preventDefault()
                  setTermsModalOpen(true)
                }}
              >
                자세히 보기
              </Button>
            </label>
          </div>
        </Card>

        <div className="flex items-center justify-between border-t pt-4">
          <div className="text-sm text-muted-foreground">2 / 2</div>
          <Button onClick={handleSubmit}>방송 등록</Button>
        </div>
      </div>

      <ProductPickerModal
        open={productPickerOpen}
        onClose={() => setProductPickerOpen(false)}
        onSave={(products) =>
          setSelectedProducts(
            products.map((p) => ({
              ...p,
              broadcastPrice: p.price,
              quantity: 1,
            })),
          )
        }
        currentlySelected={selectedProducts}
      />

      <TermsModal open={termsModalOpen} onClose={() => setTermsModalOpen(false)} />
    </div>
  )
}
